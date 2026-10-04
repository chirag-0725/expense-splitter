package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.AddMemberRequest;
import com.example.expensesplitter.dto.CreateGroupRequest;
import com.example.expensesplitter.dto.GroupResponse;
import com.example.expensesplitter.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request,
                                                       Authentication authentication) {
        GroupResponse response = groupService.createGroup(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(Authentication authentication) {
        return ResponseEntity.ok(groupService.getUserGroups(authentication.getName()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroup(groupId, authentication.getName()));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable Long groupId,
                                                     @Valid @RequestBody AddMemberRequest request,
                                                     Authentication authentication) {
        GroupResponse response = groupService.addMember(groupId, request.getName(), authentication.getName());
        return ResponseEntity.ok(response);
    }
}
