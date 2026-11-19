package com.ferreteria.ferreteria_backend.repository;

import com.ferreteria.ferreteria_backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByEmailUsuario(String emailUsuario);
}
