package com.example.uniclub.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateMonthlyActivityBulkRequest {

    private int year;
    private int month;
    private List<CreateMonthlyActivityRequest> items;
}
