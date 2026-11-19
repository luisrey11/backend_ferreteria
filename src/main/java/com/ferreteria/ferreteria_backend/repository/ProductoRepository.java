package com.ferreteria.ferreteria_backend.repository;

import com.ferreteria.ferreteria_backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
