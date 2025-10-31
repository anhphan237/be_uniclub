package com.example.uniclub.config;

import com.example.uniclub.entity.Tag;
import com.example.uniclub.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagSeeder implements CommandLineRunner {

    private final TagRepository tagRepository;

    @Override
    public void run(String... args) {
        createCoreTagIfMissing("event");
        createCoreTagIfMissing("club");
    }

    private void createCoreTagIfMissing(String name) {
        tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
    }
}
