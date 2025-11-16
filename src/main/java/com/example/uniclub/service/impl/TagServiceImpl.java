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

    @Override
    public Tag createTagIfNotExists(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tag name is required.");
        }

        // Normalize
        String normalized = name.trim().toLowerCase();

        // Check exists
        if (tagRepository.existsByNameIgnoreCase(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tag already exists.");
        }

        Tag tag = Tag.builder()
                .name(normalized)
                .build();

        return tagRepository.save(tag);
    }


    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found."));

        // Do not allow deletion of core tags
        if (tag.isCore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete core tag: " + tag.getName());
        }

        tagRepository.delete(tag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found."));

        boolean isCore = tag.isCore();

        // Nếu tag là core → CHỈ cho chỉnh sửa giá trị core
        if (isCore) {

            // Không cho đổi name
            if (request.getName() != null && !request.getName().isBlank()
                    && !request.getName().equals(tag.getName())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Core tag cannot change name.");
            }

            // Không cho đổi description
            if (request.getDescription() != null
                    && !request.getDescription().equals(tag.getDescription())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Core tag cannot change description.");
            }

            // CHỈ cho phép đổi core flag
            if (request.getCore() != null) {
                tag.setCore(request.getCore());
            }

            tagRepository.save(tag);
            return TagResponse.from(tag);
        }

        // Nếu KHÔNG phải core → cập nhật như bình thường
        // Update name (có normalize)
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

        // Optional: update core flag
        if (request.getCore() != null) {
            tag.setCore(request.getCore());
        }

        tagRepository.save(tag);
        return TagResponse.from(tag);
    }


}
