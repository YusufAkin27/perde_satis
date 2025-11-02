package perdesatis.demo.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.response.DataResponseMessage;
import perdesatis.demo.security.JwtService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<DataResponseMessage<LoginResponse>> login(@RequestBody LoginRequest request) {
        boolean isValid = adminService.validateCredentials(request.getUsername(), request.getPassword());

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(DataResponseMessage.error("Kullanıcı adı veya şifre hatalı"));
        }

        String token = jwtService.generateToken(request.getUsername());
        LoginResponse response = new LoginResponse(token, request.getUsername());

        return ResponseEntity.ok(DataResponseMessage.success("Giriş başarılı", response));
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    @lombok.AllArgsConstructor
    static class LoginResponse {
        private String token;
        private String username;
    }
}

