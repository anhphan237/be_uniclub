package com.example.uniclub.service.impl;

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
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name.toLowerCase()).build()));
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found"));

        // 🚫 Không cho xoá core tag
        if (tag.getName().equalsIgnoreCase("event") || tag.getName().equalsIgnoreCase("club")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot delete core tag: " + tag.getName());
        }

        tagRepository.delete(tag);
    }
}
