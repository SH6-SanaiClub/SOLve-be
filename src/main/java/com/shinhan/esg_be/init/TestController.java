package com.shinhan.esg_be.init;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final TestItemRepository testItemRepository;

    // 전체 조회
    @GetMapping
    public List<TestItem> getAll() {
        return testItemRepository.findAll();
    }

    // 등록
    @PostMapping
    public TestItem create(@RequestBody TestItemRequest request) {
        TestItem item = new TestItem();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        return testItemRepository.save(item);
    }
}