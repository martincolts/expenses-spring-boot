package com.tincho.expenses.services;

import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Tenant;
import com.tincho.expenses.repositories.CategoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultCategorySeeder {

    // name, iconId, colorId — must match frontend constants/categoryIcons and constants/categoryColors
    private static final List<String[]> DEFAULTS = List.of(
        new String[]{"Compras / Supermercado",          "groceries", "orange"},
        new String[]{"Comidas fuera / Delivery",         "food",      "red"},
        new String[]{"Hogar",                            "home",      "teal"},
        new String[]{"Transporte",                       "car",       "blue"},
        new String[]{"Salud y Bienestar",                "health",    "green"},
        new String[]{"Trabajo y Profesión",              "work",      "indigo"},
        new String[]{"Mascotas",                         "pets",      "amber"},
        new String[]{"Entretenimiento y Vida Social",    "movies",    "purple"},
        new String[]{"Regalos y Celebraciones",          "gifts",     "pink"},
        new String[]{"Gastos Grandes / Excepcionales",   "repairs",   "orange"},
        new String[]{"Fricción Financiera",              "bills",     "gray"},
        new String[]{"Seguro Auto",                      "car",       "teal"}
    );

    private final CategoryRepository repository;

    public void seedFor(Long tenantId) {
        AggregateReference<Tenant, Long> tenant = AggregateReference.to(tenantId);
        OffsetDateTime now = OffsetDateTime.now();
        List<Category> categories = DEFAULTS.stream()
                .map(d -> new Category(null, tenant, d[0], d[1], d[2], now))
                .toList();
        repository.saveAll(categories);
    }
}
