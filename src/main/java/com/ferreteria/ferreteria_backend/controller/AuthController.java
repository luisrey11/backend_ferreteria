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
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody Usuario nuevoUsuario) {
        if (usuarioRepository.existsByEmail(nuevoUsuario.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo completar el registro"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nuevoUsuario.getNombre());
        usuario.setEmail(nuevoUsuario.getEmail());
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

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

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
