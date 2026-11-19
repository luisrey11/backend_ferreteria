package com.ferreteria.ferreteria_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String emailUsuario;

    @Column(nullable = false)
    private Long productoId;

    private String nombreProducto;

    @Column(nullable = false)
    private Integer cantidad;

    private Double precioUnitario;

    private Double total;

    private String estado = "PENDIENTE";

    private String tipoEntrega = "RETIRO_TIENDA";

    private String direccionDespacho;

    private String metodoPago = "CONTADO";

    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
