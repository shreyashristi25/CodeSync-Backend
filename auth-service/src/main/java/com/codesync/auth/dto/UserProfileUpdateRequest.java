package com.codesync.auth.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
	    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") 
	    String username,

	    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") 
	    String fullName,

	    String avatar,

	    @Size(max = 500, message = "Bio cannot exceed 500 characters") 
	    String bio
	) {}