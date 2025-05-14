package online.bmj.www.controllers;

//import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import online.bmj.www.DTOs.MessageResponse;
import online.bmj.www.models.User;
import online.bmj.www.repository.jpa.UserRepository;
import online.bmj.www.security.jwt.JwtUtils;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class TestController {
	@Autowired
	JwtUtils jwtUtils;
	@Autowired
	UserRepository userRepository;

	@GetMapping("/all")
	public String allAccess() {
		return "Public Content.";
	}

	@PostMapping("/fetchUser")
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
	public ResponseEntity<?> fetchUser(@RequestBody String id) throws Exception {
		if (userRepository.existsById(Long.valueOf(id))) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error:11 User Id not present"));
		}

		// Create new user's account
		Optional<User> user = userRepository.findById(Long.valueOf(id));

		return ResponseEntity.ok(user);
	}

	@GetMapping("/user")
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
	public ResponseEntity<?> userAccess(@RequestHeader Map<String, String> headers) {
//		if (userRepository.existsById(id)) {
//			return ResponseEntity.badRequest().body(new MessageResponse("Error:11 User Id not present"));
//		}
		
		System.out.print(jwtUtils.getUserNameFromJwtToken(headers.get("authorization").substring(7)));
		// Create new user's account
		Optional<User> user = userRepository.findByPhone(jwtUtils.getUserNameFromJwtToken(headers.get("authorization").substring(7)));

		return ResponseEntity.ok(user);
//		return "User Content.";
	}

	@GetMapping("/mod")
	@PreAuthorize("hasRole('MODERATOR')")
	public String moderatorAccess() {
		return "Moderator Board.";
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public String adminAccess() {
		return "Admin Board.";
	}
}
