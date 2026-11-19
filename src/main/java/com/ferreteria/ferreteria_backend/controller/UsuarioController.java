package com.ferreteria.ferreteria_backend.controller;

import com.ferreteria.ferreteria_backend.model.Usuario;
import com.ferreteria.ferreteria_backend.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "CRUD completo de usuarios y gestión de roles (Requiere Rol ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private static final Set<String> ROLES_VALIDOS = Set.of("ADMIN", "VENDEDOR", "CLIENTE");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los usuarios")
    public List<Usuario> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        usuarios.forEach(u -> u.setPassword("********"));
        return usuarios;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener un usuario por su ID")
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }
        Usuario usuario = usuarioOpt.get();
        usuario.setPassword("********");
        return ResponseEntity.ok(usuario);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario con rol específico (Alta administrativa)")
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario nuevoUsuario) {
        if (nuevoUsuario.getNombre() == null || nuevoUsuario.getNombre().isBlank()
                || nuevoUsuario.getEmail() == null || nuevoUsuario.getEmail().isBlank()
                || nuevoUsuario.getPassword() == null || nuevoUsuario.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre, email y password son obligatorios"));
        }

        String emailNormalizado = nuevoUsuario.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo ya está registrado"));
        }

        String rolSolicitado = (nuevoUsuario.getRol() != null && !nuevoUsuario.getRol().isBlank())
                ? nuevoUsuario.getRol().trim().toUpperCase()
                : "CLIENTE";

        if (!ROLES_VALIDOS.contains(rolSolicitado)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol no válido. Use ADMIN, VENDEDOR o CLIENTE"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nuevoUsuario.getNombre().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));
        usuario.setRol(rolSolicitado);

        Usuario guardado = usuarioRepository.save(usuario);
        guardado.setPassword("********");
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar nombre, email, rol o contraseña de un usuario")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario datosActualizados) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }

        Usuario usuario = usuarioOpt.get();

        if (datosActualizados.getNombre() != null) {
            if (datosActualizados.getNombre().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre no puede estar vacío"));
            }
            usuario.setNombre(datosActualizados.getNombre().trim());
        }

        if (datosActualizados.getEmail() != null) {
            String nuevoEmail = datosActualizados.getEmail().trim().toLowerCase();
            if (nuevoEmail.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El correo no puede estar vacío"));
            }
            if (!nuevoEmail.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(nuevoEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error", "El correo ya está registrado"));
            }
            usuario.setEmail(nuevoEmail);
        }

        if (datosActualizados.getRol() != null && !datosActualizados.getRol().isBlank()) {
            String nuevoRol = datosActualizados.getRol().trim().toUpperCase();
            if (!ROLES_VALIDOS.contains(nuevoRol)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rol no válido. Use ADMIN, VENDEDOR o CLIENTE"));
            }
            usuario.setRol(nuevoRol);
        }

        if (datosActualizados.getPassword() != null && !datosActualizados.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(datosActualizados.getPassword()));
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        actualizado.setPassword("********");
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un usuario por su ID")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }
}