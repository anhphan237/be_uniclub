package com.example.uniclub.service;

import java.util.List;
import java.util.Map;

public interface AdminStatisticService {
    List<Map<String, Object>> getStudentCountByMajor();
}
