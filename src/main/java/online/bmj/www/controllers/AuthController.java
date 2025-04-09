package online.bmj.www.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.chargebee.models.Customer;

import jakarta.validation.Valid;
import online.bmj.www.DTOs.JwtResponse;
import online.bmj.www.DTOs.LoginRequest;
import online.bmj.www.DTOs.MessageResponse;
import online.bmj.www.DTOs.SignupRequest;
import online.bmj.www.models.User;
import online.bmj.www.security.jwt.JwtUtils;
import online.bmj.www.services.UserDetailsImpl;
import online.bmj.www.services.UserDetailsServiceImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    // @Autowired
    // private ShopifyAuthService shopifyAuthService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getPhone(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(),
                        userDetails.getEmail(), roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) throws Exception {
        if (userDetailsService.existsByEmail(signUpRequest.user.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        if (userDetailsService.existsByPhone(signUpRequest.user.getPhone())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Phone is already in use!"));
        }

        User user = signUpRequest.getUser();
        user.setPassword(signUpRequest.user.getPassword());

        try {
            Customer.create()
                    .id(generateId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .billingAddressFirstName(user.getFirstName())
                    .billingAddressLastName(user.getLastName())
                    .billingAddressLine1(user.getAddress())
                    .billingAddressLine2(user.getExtendedAddr())
                    .billingAddressLine3(user.getExtendedAddr2())
                    .billingAddressLine3(user.getCity())
                    .billingAddressLine3(user.getState())
                    .billingAddressZip(user.getZip())
                    .billingAddressCountry(user.getCountry())
                    .preferredCurrencyCode("INR")
                    .request();
        } catch (Exception e) {
            return ResponseEntity.ok(new MessageResponse(e.getMessage()));
        }

        userDetailsService.saveUser(user);
        return ResponseEntity.ok("ok");
    }

    private String generateId() {
        if (userDetailsService.getUserCount() == 0) {
            return "501";
        }
        return String.valueOf(userDetailsService.getUserCount() + 501);
    }

    // @PostMapping("/shopify/signin")
    // public ResponseEntity<?> shopifyAuthenticate(@Valid @RequestBody ShopifyLoginRequest request) {
    //     try {
    //         String shopifyToken = shopifyAuthService.authenticateCustomer(
    //                 request.getEmail(),
    //                 request.getPassword()
    //         );

    //         if (shopifyToken == null) {
    //             return unauthorizedResponse("Invalid Shopify credentials");
    //         }
    //         Map<String, Object> customer = shopifyAuthService.getCustomerDetails(shopifyToken);
    //         User user = userDetailsService.findByEmail(customer.get("email").toString()).orElseGet(() -> createShopifyUser(customer));

    //         return generateJwtResponse(user);

    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(new MessageResponse("Shopify authentication failed: " + e.getMessage()));
    //     }
    // }

    // @Option
    // @PostMapping("/customer-created")
    @RequestMapping(value="/customer-created", method = RequestMethod.OPTIONS, consumes = "application/json")   
    @ResponseBody
    public ResponseEntity<?> handleCustomerCreate(@RequestBody Map<String, Object> payload) {
        if (userDetailsService.existsByEmail(payload.get("email").toString())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        if (userDetailsService.existsByPhone(payload.get("phone").toString())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Phone is already in use!"));
        }

        User user = new User();

        try {
            user.setEmail(payload.get("email").toString());
            user.setPhone(payload.get("phone").toString());
            user.setPassword(passwordEncoder.encode(payload.get("password").toString()));
            user.setLastName(payload.get("last_name").toString());
            user.setFirstName(payload.get("first_name").toString());
            user.setCity(payload.get("city").toString());
            user.setAddress(payload.get("address").toString());
            user.setExtendedAddr(payload.get("address1").toString());
            user.setExtendedAddr2(payload.get("address2").toString());

            user.setCountry(payload.get("country").toString());
            user.setState(payload.get("state").toString());
            user.setZip(payload.get("zip").toString());
            // user.setEnabled(true);

            // String email = (String) payload.get("email");
            // String firstName = (String) payload.get("first_name");
            // String lastName = (String) payload.get("last_name");
            // String phone = (String) payload.get("phone");
            // List<Map<String, String>> noteAttributes = (List<Map<String, String>>) payload.get("note_attributes");
            // Map<String, String> flatAddress = new HashMap<>();
            // for (Map<String, String> attr : noteAttributes) {
            //     flatAddress.put(attr.get("name"), attr.get("value"));
            // }
            // Create Chargebee customer
            Customer.create()
                    .id(generateId()) // Or use Shopify ID if preferred
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .billingAddressFirstName(user.getFirstName())
                    .billingAddressLastName(user.getLastName())
                    .billingAddressLine1(user.getAddress())
                    .billingAddressLine2(user.getExtendedAddr())
                    .billingAddressLine3(user.getExtendedAddr2())
                    .billingAddressCity(user.getExtendedAddr2())
                    .billingAddressState(user.getState())
                    .billingAddressZip(user.getZip())
                    .billingAddressCountry(user.getCountry())
                    .preferredCurrencyCode("INR")
                    .request();

            return ResponseEntity.ok("Chargebee customer created");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // @PostMapping("/shopify/signup")
    // public ResponseEntity<?> shopifyRegister(@Valid @RequestBody ShopifySignupRequest request) {
    //     if (userDetailsService.existsByEmail(request.getEmail())) {
    //         return ResponseEntity.badRequest().body(new MessageResponse("Email already registered"));
    //     }

    //     try {
    //         Map<String, Object> creationResult = shopifyAuthService.createCustomer(request);
    //         if (!creationResult.get("userErrors").toString().isEmpty()) {
    //             return ResponseEntity.badRequest().body("Shopify registration failed");
    //         }

    //         User user = createLocalUser(request);
    //         createChargebeeCustomer(user);
    //         userDetailsService.saveUser(user);

    //         return ResponseEntity.ok(new MessageResponse("Shopify registration successful"));

    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(new MessageResponse("Registration error: " + e.getMessage()));
    //     }
    // }

    // private User createShopifyUser(Map<String, Object> customer) {
    //     User user = new User();
    //     user.setEmail(customer.get("email").toString());
    //     user.setFirstName(customer.get("firstName").toString());
    //     user.setLastName(customer.get("lastName").toString());
    //     user.setPhone(customer.get("phone").toString());
    //     user.setPassword(passwordEncoder.encode("shopify_authenticated"));

    //     // Handle address
    //     Map<String, Object> address = extractAddress(customer);
    //     if (address != null) {
    //         user.setAddress(address.get("address1").toString());
    //         user.setCity(address.get("city").toString());
    //         user.setCountry(address.get("country").toString());
    //         user.setZip(address.get("zip").toString());
    //     }
    //     return userDetailsService.saveUser(user);
    // }

    // private boolean createChargebeeCustomer(User user) {
    //     try {
    //         Customer.create()
    //                 .id(generateId())
    //                 .firstName(user.getFirstName())
    //                 .lastName(user.getLastName())
    //                 .email(user.getEmail())
    //                 .phone(user.getPhone())
    //                 .billingAddressFirstName(user.getFirstName())
    //                 .billingAddressLastName(user.getLastName())
    //                 .billingAddressLine1(user.getAddress())
    //                 .billingAddressCity(user.getCity())
    //                 .billingAddressState(user.getState())
    //                 .billingAddressZip(user.getZip())
    //                 .billingAddressCountry(user.getCountry())
    //                 .preferredCurrencyCode("INR") // Set your default currency
    //                 .request();
    //         return true;
    //     } catch (Exception e) {
    //         logger.error("Chargebee customer creation failed for user {}: {}", user.getEmail(), e.getMessage());
    //         return false;
    //     }
    // }

    // private User createLocalUser(ShopifySignupRequest request) {
    //     User user = new User();

    //     // Map fields from Shopify signup request
    //     user.setEmail(request.getEmail());
    //     user.setFirstName(request.getFirstName());
    //     user.setLastName(request.getLastName());
    //     user.setPhone(request.getPhone());
    //     user.setAddress(request.getAddress());
    //     user.setCity(request.getCity());
    //     user.setState(request.getState());
    //     user.setZip(request.getZip());
    //     user.setCountry(request.getCountry());

    //     // Set default password (can be changed later)
    //     user.setPassword(passwordEncoder.encode("shopify_authenticated"));

    //     // Set additional fields if needed
    //     // user.setEnabled(true);
    //     // user.setCreatedAt(LocalDateTime.now());
    //     return userDetailsService.saveUser(user);
    // }

    // private Map<String, Object> extractAddress(Map<String, Object> customer) {
    //     try {
    //         Map<String, Object> addresses = (Map<String, Object>) customer.get("addresses");
    //         List<Map<String, Object>> edges = (List<Map<String, Object>>) addresses.get("edges");
    //         if (!edges.isEmpty()) {
    //             return (Map<String, Object>) edges.get(0).get("node");
    //         }
    //     } catch (Exception e) {
    //         logger.error("Error extracting address: {}", e.getMessage());
    //     }
    //     return null;
    // }

    // private ResponseEntity<?> generateJwtResponse(User user) {
    //     UserDetailsImpl userDetails = UserDetailsImpl.build(user);
    //     List<String> roles = userDetails.getAuthorities().stream()
    //             .map(GrantedAuthority::getAuthority)
    //             .collect(Collectors.toList());

    //     Authentication authentication = new UsernamePasswordAuthenticationToken(
    //             userDetails, null, userDetails.getAuthorities());

    //     String jwt = jwtUtils.generateJwtToken(authentication);

    //     return ResponseEntity.ok(new JwtResponse(
    //             jwt,
    //             userDetails.getId(),
    //             userDetails.getUsername(),
    //             userDetails.getEmail(),
    //             roles
    //     ));
    // }

    // private ResponseEntity<?> unauthorizedResponse(String message) {
    //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
    //             .body(new MessageResponse(message));
    // }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
