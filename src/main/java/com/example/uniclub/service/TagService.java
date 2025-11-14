package com.example.uniclub.service;

import com.example.uniclub.dto.request.TagRequest;
import com.example.uniclub.dto.response.TagResponse;
import com.example.uniclub.entity.Tag;
import java.util.List;

public interface TagService {
    Tag createTagIfNotExists(String name);
    List<Tag> getAllTags();
    void deleteTag(Long tagId);
    TagResponse updateTag(Long id, TagRequest request);

}
