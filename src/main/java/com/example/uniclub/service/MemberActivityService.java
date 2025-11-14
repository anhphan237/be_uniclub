
package com.example.uniclub.service;

import java.time.YearMonth;

public interface MemberActivityService {

    void recalculateForClubAndMonth(Long clubId, YearMonth month);

    void recalculateForAllClubsAndMonth(YearMonth month);
}
