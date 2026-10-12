package com.forvmom.core.services;

import com.forvmom.common.dto.request.SupportQueryRequestDto;
import com.forvmom.common.dto.response.SupportQueryResponseDto;

import java.util.List;

public interface SupportQueryService {

    /**
     * Submits a contact/support query.
     *
     * <p>
     * When {@code userId} is present (logged-in user, resolved from the
     * gateway's {@code X-User-Id} header), name/email/phone are auto-filled from
     * the user's profile and the query is linked to that account. When absent
     * (guest), {@code name}, {@code email} and {@code phone} must be supplied on
     * the request.
     *
     * @param requestDto the submitted query
     * @param userId the caller identity from gateway; app-user id preferred
     * @return the created query
     */
    SupportQueryResponseDto submitQuery(SupportQueryRequestDto requestDto, Long userId);

    /** All queries for the admin list, newest first, optionally filtered by status */
    List<SupportQueryResponseDto> getAllQueriesAdmin(String status);

    SupportQueryResponseDto getQueryById(Long id);

    /** Marks a query resolved, stamping the resolution time */
    SupportQueryResponseDto markResolved(Long id);

    /** Queries submitted by the logged-in user identified by the gateway header */
    List<SupportQueryResponseDto> getMyQueries(Long userId);
}
