package com.ferreteria.ferreteria_backend.controller;

import com.ferreteria.ferreteria_backend.model.Usuario;
import com.ferreteria.ferreteria_backend.repository.UsuarioRepository;
import com.ferreteria.ferreteria_backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody Usuario nuevoUsuario) {
        if (nuevoUsuario.getNombre() == null || nuevoUsuario.getNombre().isBlank()
                || nuevoUsuario.getEmail() == null || nuevoUsuario.getEmail().isBlank()
                || nuevoUsuario.getPassword() == null || nuevoUsuario.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre, email y password son obligatorios"));
        }

        String emailNormalizado = nuevoUsuario.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo completar el registro"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nuevoUsuario.getNombre().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));
        usuario.setRol("CLIENTE");

        usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Usuario registrado correctamente",
                "email", usuario.getEmail(),
                "rol", usuario.getRol()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        String email = credenciales.get("email");
        String password = credenciales.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.trim().toLowerCase());

        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(password, usuarioOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "rol", usuario.getRol(),
                "nombre", usuario.getNombre()
        ));
    }
}
