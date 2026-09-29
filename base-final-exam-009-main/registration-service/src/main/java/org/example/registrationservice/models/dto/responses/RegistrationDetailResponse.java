package org.example.registrationservice.models.dto.responses;

public record RegistrationDetailResponse(
        Long id,

        Long eventId,

        Double ticketPrice,

        Double lineTotal
) {
}
