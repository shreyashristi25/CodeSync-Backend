package com.codesync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	    @NotBlank(message = "Email is required") 
	    @Email(message = "Please provide a valid email address") 
	    String email,

	    @NotBlank(message = "Username is required") 
	    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") 
	    String username,

	    @NotBlank(message = "Full name is required") 
	    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") 
	    String fullName,

	    @NotBlank(message = "Password is required") 
	    @Size(min = 8, max = 120, message = "Password must be at least 8 characters long") 
	    String password,

	    String avatar,

	    @Size(max = 500, message = "Bio cannot exceed 500 characters") 
	    String bio
	) {}