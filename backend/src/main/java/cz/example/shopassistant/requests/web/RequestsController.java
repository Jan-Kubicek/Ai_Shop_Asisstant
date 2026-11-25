package cz.example.shopassistant.requests.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zatím pouze prázdný seznam. Plná implementace modulu Requests přijde
 * v další fázi; endpoint drží stejný kontrakt jako frontend.
 */
@RestController
@RequestMapping("/api/v1/requests")
public class RequestsController {

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public PageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return new PageResponse(List.of(), 0, safePage, safeSize);
    }

    public record PageResponse(List<Object> items, long total, int page, int size) {
    }
}
