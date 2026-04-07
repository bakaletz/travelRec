package com.travelRec.mapper;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Rating;
import com.travelRec.entity.Trip;
import com.travelRec.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public Rating toEntity(QuickRatingRequest request, User user, Trip trip, City city) {
        return Rating.builder()
                .user(user)
                .trip(trip)
                .city(city)
                .overallScore(request.getOverallScore())
                .feedback(request.getFeedback())
                .build();
    }

    public Rating toEntity(DetailedRatingRequest request, User user, Trip trip, City city) {
        return Rating.builder()
                .user(user)
                .trip(trip)
                .city(city)
                .overallScore(request.getOverallScore())
                .cultureRating(request.getCultureRating())
                .foodRating(request.getFoodRating())
                .nightlifeRating(request.getNightlifeRating())
                .natureRating(request.getNatureRating())
                .safetyRating(request.getSafetyRating())
                .costRating(request.getCostRating())
                .beachRating(request.getBeachRating())
                .architectureRating(request.getArchitectureRating())
                .shoppingRating(request.getShoppingRating())
                .feedback(request.getFeedback())
                .build();
    }

    public RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .tripId(rating.getTrip().getId())
                .cityId(rating.getCity().getId())
                .cityName(rating.getCity().getName())
                .overallScore(rating.getOverallScore())
                .cultureRating(rating.getCultureRating())
                .foodRating(rating.getFoodRating())
                .nightlifeRating(rating.getNightlifeRating())
                .natureRating(rating.getNatureRating())
                .safetyRating(rating.getSafetyRating())
                .costRating(rating.getCostRating())
                .beachRating(rating.getBeachRating())
                .architectureRating(rating.getArchitectureRating())
                .shoppingRating(rating.getShoppingRating())
                .feedback(rating.getFeedback())
                .detailed(rating.isDetailed())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
