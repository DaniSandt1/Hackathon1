package com.oreo.insight_factory.sales;

import com.oreo.insight_factory.security.JwtService;
import com.oreo.insight_factory.users.User;
import com.oreo.insight_factory.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private User getUser(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);
        return userRepository.findByUsername(username).orElseThrow();
    }

    // 🟢 Crear venta
    @PostMapping
    public ResponseEntity<?> create(@RequestHeader("Authorization") String auth,
                                    @RequestBody Sale sale) {
        User user = getUser(auth);

        // 🔒 Si es BRANCH, solo puede crear ventas de su sucursal
        if (user.getRole().name().equals("BRANCH") &&
                !sale.getBranch().equalsIgnoreCase(user.getBranch())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ No puedes crear ventas para otra sucursal");
        }

        Sale saved = salesService.create(sale, user);
        return ResponseEntity.status(201).body(saved);
    }

    // 🟡 Listar ventas
    @GetMapping
    public ResponseEntity<List<Sale>> list(@RequestHeader("Authorization") String auth,
                                           @RequestParam(required = false) String branch,
                                           @RequestParam(required = false) Instant from,
                                           @RequestParam(required = false) Instant to) {
        User user = getUser(auth);
        return ResponseEntity.ok(salesService.list(branch, from, to, user));
    }

    // 🟢 Obtener venta por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@RequestHeader("Authorization") String auth,
                                     @PathVariable String id) {
        User user = getUser(auth);
        Sale sale = salesService.get(id);

        // 🔒 BRANCH solo puede ver sus propias ventas
        if (user.getRole().name().equals("BRANCH") &&
                !sale.getBranch().equalsIgnoreCase(user.getBranch())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ No puedes acceder a ventas de otra sucursal");
        }

        return ResponseEntity.ok(sale);
    }

    // 🔵 Actualizar venta
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestHeader("Authorization") String auth,
                                    @PathVariable String id,
                                    @RequestBody Sale updated) {
        User user = getUser(auth);
        Sale existing = salesService.get(id);

        // 🔒 Validaciones de permiso
        if (user.getRole().name().equals("BRANCH") &&
                !existing.getBranch().equalsIgnoreCase(user.getBranch())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ No puedes actualizar ventas de otra sucursal");
        }

        Sale saved = salesService.update(id, updated, user);
        return ResponseEntity.ok(saved);
    }

    // 🔴 Eliminar venta
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String auth,
                                    @PathVariable String id) {
        User user = getUser(auth);

        if (!user.getRole().name().equals("CENTRAL")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Solo CENTRAL puede eliminar ventas");
        }

        salesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
