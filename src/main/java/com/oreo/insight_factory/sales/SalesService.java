package com.oreo.insight_factory.sales;

import com.oreo.insight_factory.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;

    public Sale create(Sale sale, User user) {
        sale.setCreatedBy(user);
        return salesRepository.save(sale);
    }

    public List<Sale> list(String branch, Instant from, Instant to, User user) {
        // Si el usuario es BRANCH, solo puede ver su sucursal
        if (user.getRole().name().equals("BRANCH")) {
            branch = user.getBranch();
        }
        return salesRepository.findByFilters(branch, from, to);
    }
    public Sale update(String id, Sale updated, User user) {
        Sale existing = salesRepository.findById(id).orElseThrow();

        existing.setSku(updated.getSku());
        existing.setUnits(updated.getUnits());
        existing.setPrice(updated.getPrice());
        existing.setSoldAt(updated.getSoldAt());
        // Solo CENTRAL puede cambiar branch
        if (user.getRole().name().equals("CENTRAL")) {
            existing.setBranch(updated.getBranch());
        }

        return salesRepository.save(existing);
    }

    public void delete(String id) {
        salesRepository.deleteById(id);
    }


    public Sale get(String id) {
        return salesRepository.findById(id).orElseThrow();
    }
}
