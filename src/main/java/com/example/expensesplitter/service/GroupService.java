package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.CreateGroupRequest;
import com.example.expensesplitter.dto.GroupResponse;
import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.GroupMember;
import com.example.expensesplitter.entity.User;
import com.example.expensesplitter.exception.DuplicateResourceException;
import com.example.expensesplitter.exception.ForbiddenOperationException;
import com.example.expensesplitter.exception.ResourceNotFoundException;
import com.example.expensesplitter.repository.ExpenseGroupRepository;
import com.example.expensesplitter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final ExpenseGroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(ExpenseGroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String currentUsername) {
        User creator = getUserOrThrow(currentUsername);
        ExpenseGroup group = new ExpenseGroup(request.getName(), creator);

        // De-dupe names case-insensitively, creator always included first.
        Set<String> seen = new LinkedHashSet<>();
        seen.add(creator.getUsername().toLowerCase());
        group.addMember(buildMember(creator.getUsername()));

        if (request.getMembers() != null) {
            for (String raw : request.getMembers()) {
                if (raw == null || raw.isBlank()) continue;
                String name = raw.trim();
                if (!seen.add(name.toLowerCase())) continue; // skip duplicates
                group.addMember(buildMember(name));
            }
        }

        ExpenseGroup saved = groupRepository.save(group);
        return toResponse(saved);
    }

    @Transactional
    public GroupResponse addMember(Long groupId, String name, String currentUsername) {
        ExpenseGroup group = getGroupOrThrow(groupId);
        requireMembership(group, currentUsername);

        String trimmed = name.trim();
        boolean exists = group.getMembers().stream()
                .anyMatch(m -> m.getDisplayName().equalsIgnoreCase(trimmed));
        if (exists) {
            throw new DuplicateResourceException("'" + trimmed + "' is already a member of this group");
        }

        group.addMember(buildMember(trimmed));
        ExpenseGroup saved = groupRepository.save(group);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long groupId, String currentUsername) {
        ExpenseGroup group = getGroupOrThrow(groupId);
        requireMembership(group, currentUsername);
        return toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(String currentUsername) {
        User user = getUserOrThrow(currentUsername);
        return groupRepository.findAllByMemberUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** A name matching a registered username is linked to that account; otherwise it's a guest member. */
    private GroupMember buildMember(String name) {
        GroupMember member = new GroupMember();
        Optional<User> existing = userRepository.findByUsername(name);
        if (existing.isPresent()) {
            member.setUser(existing.get());
            member.setDisplayName(existing.get().getUsername());
        } else {
            member.setDisplayName(name);
        }
        return member;
    }

    ExpenseGroup getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /** Authorization check: does this *logged-in account* belong to the group? Guests can't call the API at all. */
    void requireMembership(ExpenseGroup group, String username) {
        boolean isMember = group.getMembers().stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getUsername().equals(username));
        if (!isMember) {
            throw new ForbiddenOperationException("You are not a member of group: " + group.getName());
        }
    }

    /** Resolve any member of the group (guest or linked) by their display name. */
    GroupMember resolveMemberOrThrow(ExpenseGroup group, String name) {
        String trimmed = name.trim();
        return group.getMembers().stream()
                .filter(m -> m.getDisplayName().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "'" + trimmed + "' is not a member of group: " + group.getName()));
    }

    /** Resolve the GroupMember row that corresponds to the currently logged-in account. */
    GroupMember resolveMemberForUsername(ExpenseGroup group, String username) {
        return group.getMembers().stream()
                .filter(m -> m.getUser() != null && m.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of group: " + group.getName()));
    }

    private GroupResponse toResponse(ExpenseGroup group) {
        List<String> memberNames = group.getMembers().stream()
                .map(GroupMember::getDisplayName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getCreatedBy().getUsername(),
                memberNames,
                group.getCreatedAt()
        );
    }
}
