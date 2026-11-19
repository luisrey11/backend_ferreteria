package com.ferreteria.ferreteria_backend.controller;

import com.ferreteria.ferreteria_backend.model.Pedido;
import com.ferreteria.ferreteria_backend.model.Producto;
import com.ferreteria.ferreteria_backend.repository.PedidoRepository;
import com.ferreteria.ferreteria_backend.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE", "CONFIRMADO", "DESPACHADO", "ENTREGADO", "CANCELADO"
    );

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    private boolean tieneRolStaff(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDEDOR"));
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listarPedidos(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (tieneRolStaff(auth)) {
            return ResponseEntity.ok(pedidoRepository.findAll());
        }
        return ResponseEntity.ok(pedidoRepository.findByEmailUsuario(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPedido(@PathVariable Long id, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Pedido no encontrado"));
        }
        Pedido pedido = pedidoOpt.get();
        if (!tieneRolStaff(auth) && !auth.getName().equalsIgnoreCase(pedido.getEmailUsuario())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No tienes permiso para ver este pedido"));
        }
        return ResponseEntity.ok(pedido);
    }

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Pedido solicitud, Authentication auth) {
        if (solicitud.getProductoId() == null || solicitud.getCantidad() == null || solicitud.getCantidad() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe indicar un productoId y una cantidad mayor a 0"));
        }

        Optional<Producto> productoOpt = productoRepository.findById(solicitud.getProductoId());
        if (productoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Producto no encontrado"));
        }

        Producto producto = productoOpt.get();
        int stockDisponible = producto.getStock() != null ? producto.getStock() : 0;

        if (solicitud.getCantidad() > stockDisponible) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Stock insuficiente para completar el pedido",
                    "stockDisponible", stockDisponible,
                    "cantidadSolicitada", solicitud.getCantidad()
            ));
        }

        producto.setStock(stockDisponible - solicitud.getCantidad());
        productoRepository.save(producto);

        double precioUnitario = producto.getPrecio() != null ? producto.getPrecio() : 0.0;
        Pedido pedido = new Pedido();
        pedido.setEmailUsuario(auth != null ? auth.getName() : "invitado@ferreteria.cl");
        pedido.setProductoId(producto.getId());
        pedido.setNombreProducto(producto.getNombre());
        pedido.setCantidad(solicitud.getCantidad());
        pedido.setPrecioUnitario(precioUnitario);
        pedido.setTotal(precioUnitario * solicitud.getCantidad());
        pedido.setEstado("PENDIENTE");
        if (solicitud.getTipoEntrega() != null && !solicitud.getTipoEntrega().isBlank()) {
            pedido.setTipoEntrega(solicitud.getTipoEntrega());
        }
        pedido.setDireccionDespacho(solicitud.getDireccionDespacho());
        if (solicitud.getMetodoPago() != null && !solicitud.getMetodoPago().isBlank()) {
            pedido.setMetodoPago(solicitud.getMetodoPago());
        }

        Pedido guardado = pedidoRepository.save(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @RequestMapping(value = "/{id}/estado", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        if (!tieneRolStaff(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo ADMIN o VENDEDOR pueden cambiar el estado"));
        }

        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe enviar el campo estado"));
        }

        String estadoNormalizado = nuevoEstado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Estado no válido",
                    "estadosPermitidos", ESTADOS_VALIDOS
            ));
        }

        Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Pedido no encontrado"));
        }

        Pedido pedido = pedidoOpt.get();
        pedido.setEstado(estadoNormalizado);
        return ResponseEntity.ok(pedidoRepository.save(pedido));
    }
}
