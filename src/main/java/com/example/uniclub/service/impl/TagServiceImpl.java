package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.TagRequest;
import com.example.uniclub.dto.response.TagResponse;
import com.example.uniclub.entity.Tag;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.TagRepository;
import com.example.uniclub.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    // =====================================================
    // 1) AUTO-CREATE (system only)
    // =====================================================
    @Override
    public Tag createTagIfNotExists(String name) {

        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tag name is required.");
        }

        String normalized = name.trim().toLowerCase();

        if (tagRepository.existsByNameIgnoreCase(normalized)) {
            return tagRepository.findByNameIgnoreCase(normalized).get();
        }

        Tag tag = Tag.builder()
                .name(normalized)
                .core(false)
                .build();

        return tagRepository.save(tag);
    }

    // =====================================================
    // 2) CREATE TAG (admin / staff)
    // =====================================================
    @Override
    public Tag createTag(TagRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tag name is required.");
        }

        String normalized = request.getName().trim().toLowerCase();

        if (tagRepository.existsByNameIgnoreCase(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tag name already exists.");
        }

        Tag tag = Tag.builder()
                .name(normalized)
                .description(request.getDescription())
                .core(request.getCore() != null ? request.getCore() : false)
                .build();

        return tagRepository.save(tag);
    }

    // =====================================================
    // 3) GET ALL TAGS
    // =====================================================
    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    // =====================================================
    // 4) DELETE TAG
    // =====================================================
    @Override
    @Transactional
    public void deleteTag(Long tagId) {

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found."));

        if (tag.isCore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete core tag: " + tag.getName());
        }

        tagRepository.delete(tag);
    }

    // =====================================================
    // 5) UPDATE TAG
    // =====================================================
    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found."));

        boolean isCore = tag.isCore();

        // ===== RULE: core tag cannot change name or description =====
        if (isCore) {

            if (request.getName() != null && !request.getName().equals(tag.getName())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Core tag cannot change name.");
            }

            if (request.getDescription() != null &&
                    !request.getDescription().equals(tag.getDescription())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Core tag cannot change description.");
            }

            if (request.getCore() != null) {
                tag.setCore(request.getCore());
            }

            return TagResponse.from(tagRepository.save(tag));
        }

        // ===== NORMAL TAG UPDATE =====

        // Update name
        if (request.getName() != null && !request.getName().isBlank()) {
            String normalized = request.getName().trim().toLowerCase();

            if (tagRepository.existsByNameIgnoreCaseAndTagIdNot(normalized, id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Tag name already exists.");
            }

            tag.setName(normalized);
        }

        // Update description
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }

        // Update core flag
        if (request.getCore() != null) {
            tag.setCore(request.getCore());
        }

        return TagResponse.from(tagRepository.save(tag));
    }
}
