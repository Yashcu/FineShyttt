package com.yash.fineshyttt.service.product;

import com.yash.fineshyttt.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlugService {

    private final ProductRepository productRepository;

    public String generate(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\w\\s-]", "")
                .trim()
                .replaceAll("[\\s_-]+", "-")
                .toLowerCase(Locale.ROOT);

        return normalized;
    }

    public String generateUnique(String name) {
        String baseSlug = generate(name);
        String candidate = baseSlug;

        int suffix = 1;
        while (productRepository.existsBySlug(candidate)) {
            suffix++;
            candidate = baseSlug + "-" + suffix;
        }

        return candidate;
    }
}
