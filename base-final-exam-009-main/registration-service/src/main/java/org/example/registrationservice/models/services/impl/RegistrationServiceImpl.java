package org.example.registrationservice.models.services.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationDetailResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.entities.Registration;
import org.example.registrationservice.models.entities.RegistrationDetail;
import org.example.registrationservice.models.repositories.RegistrationDetailRepository;
import org.example.registrationservice.models.repositories.RegistrationRepository;
import org.example.registrationservice.models.services.RegistrationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final RegistrationDetailRepository registrationDetailRepository;
    private final EventGatewayService eventGatewayService;

    @Override
    @Transactional
    public RegistrationResponse createRegistration(CreateRegistrationRequest request) {
        List<ResolvedItem> resolvedItems = request.items().stream()
                .map(this::resolveEvent)
                .toList();

        double total = resolvedItems.stream()
                .mapToDouble(item -> item.event().ticketPrice() * item.request().quantity())
                .sum();

        Registration reservation = registrationRepository.save(Registration.builder()
                .participantName(request.participantName().trim())
                .participantEmail(request.participantEmail().trim())
                .totalAmount(total)
                .status(RegistrationStatus.PENDING)
                .build());

        List<RegistrationDetail> details = resolvedItems.stream()
                .map(item -> RegistrationDetail.builder()
                        .registration(reservation)
                        .eventId(item.event().id())
                        .quantity(item.request().quantity())
                        .ticketPrice(item.event().ticketPrice())
                        .build())
                .toList();

        List<RegistrationDetail> savedDetails = registrationDetailRepository.saveAll(details);
        List<RegistrationDetailResponse> itemResponses = getRegistrationDetailResponses(savedDetails, resolvedItems);

        return new RegistrationResponse(
                reservation.getId(),
                reservation.getParticipantName(),
                reservation.getParticipantEmail(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                itemResponses);
    }

//    private static @NonNull List<RegistrationDetailResponse> getRegistrationDetailResponses(List<RegistrationDetail> savedDetails,
//                                                                                          List<ResolvedItem> resolvedItems) {
//        List<RegistrationDetailResponse> itemResponses = new ArrayList<>(savedDetails.size());
//
//        for (int index = 0; index < savedDetails.size(); index++) {
//            RegistrationDetail detail = savedDetails.get(index);
//            EventResponse event = resolvedItems.get(index).event();
//            itemResponses.add(new RegistrationDetailResponse(
//                    detail.getId(),
//                    detail.getEventId(),
//                    event.id(),
//                    event.eventName(),
//                    event.ticketPrice(),
//                    event.availableTickets(),
//                    detail.getTicketPrice(),
//                    detail.getTicketPrice() * detail.getTicketPrice())));
//
//        }
//        return itemResponses;
//    }

    private ResolvedItem resolveEvent(CreateRegistrationDetailRequest item) {
        EventResponse event = eventGatewayService.getEventById(item.eventId());
        return new ResolvedItem(item, event);
    }

    private record ResolvedItem(CreateRegistrationDetailRequest request, EventResponse event) {
    }
}
