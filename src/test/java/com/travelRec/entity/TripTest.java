package com.travelRec.entity;

import com.travelRec.entity.enums.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripTest {

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = Trip.builder()
                .id(1L)
                .name("Summer Europe")
                .status(TripStatus.PLANNED)
                .build();
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("should change status from PLANNED to COMPLETED")
        void shouldComplete() {
            trip.complete();
            assertEquals(TripStatus.COMPLETED, trip.getStatus());
        }

        @Test
        @DisplayName("should throw when completing already COMPLETED trip")
        void shouldThrowWhenAlreadyCompleted() {
            trip.setStatus(TripStatus.COMPLETED);
            assertThrows(IllegalStateException.class, () -> trip.complete());
        }

        @Test
        @DisplayName("should throw when completing RATED trip")
        void shouldThrowWhenRated() {
            trip.setStatus(TripStatus.RATED);
            assertThrows(IllegalStateException.class, () -> trip.complete());
        }

        @Test
        @DisplayName("should throw when completing CANCELLED trip")
        void shouldThrowWhenCancelled() {
            trip.setStatus(TripStatus.CANCELLED);
            assertThrows(IllegalStateException.class, () -> trip.complete());
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("should change status from PLANNED to CANCELLED")
        void shouldCancel() {
            trip.cancel();
            assertEquals(TripStatus.CANCELLED, trip.getStatus());
        }

        @Test
        @DisplayName("should throw when cancelling COMPLETED trip")
        void shouldThrowWhenCompleted() {
            trip.setStatus(TripStatus.COMPLETED);
            assertThrows(IllegalStateException.class, () -> trip.cancel());
        }

        @Test
        @DisplayName("should throw when cancelling RATED trip")
        void shouldThrowWhenRated() {
            trip.setStatus(TripStatus.RATED);
            assertThrows(IllegalStateException.class, () -> trip.cancel());
        }
    }

    @Nested
    @DisplayName("markAsRated()")
    class MarkAsRated {

        @Test
        @DisplayName("should change status from COMPLETED to RATED")
        void shouldMarkAsRated() {
            trip.setStatus(TripStatus.COMPLETED);
            trip.markAsRated();
            assertEquals(TripStatus.RATED, trip.getStatus());
        }

        @Test
        @DisplayName("should throw when rating PLANNED trip")
        void shouldThrowWhenPlanned() {
            assertThrows(IllegalStateException.class, () -> trip.markAsRated());
        }

        @Test
        @DisplayName("should throw when rating already RATED trip")
        void shouldThrowWhenAlreadyRated() {
            trip.setStatus(TripStatus.RATED);
            assertThrows(IllegalStateException.class, () -> trip.markAsRated());
        }

        @Test
        @DisplayName("should throw when rating CANCELLED trip")
        void shouldThrowWhenCancelled() {
            trip.setStatus(TripStatus.CANCELLED);
            assertThrows(IllegalStateException.class, () -> trip.markAsRated());
        }
    }
}
