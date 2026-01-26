package ilp.ilp_cw.ilp_1_2.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilityTest {

    @Test
    void testNoArgsConstructor() {
        Availability availability = new Availability();
        assertNotNull(availability);
        assertNull(availability.getDayOfWeek());
        assertNull(availability.getFromTime());
        assertNull(availability.getUntilTime());
    }

    @Test
    void testAllArgsConstructor() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertEquals("MONDAY", availability.getDayOfWeek());
        assertEquals(fromTime, availability.getFromTime());
        assertEquals(untilTime, availability.getUntilTime());
    }

    @Test
    void testSettersAndGetters() {
        Availability availability = new Availability();
        LocalTime fromTime = LocalTime.of(10, 30);
        LocalTime untilTime = LocalTime.of(18, 45);

        availability.setDayOfWeek("FRIDAY");
        availability.setFromTime(fromTime);
        availability.setUntilTime(untilTime);

        assertEquals("FRIDAY", availability.getDayOfWeek());
        assertEquals(fromTime, availability.getFromTime());
        assertEquals(untilTime, availability.getUntilTime());
    }

    @Test
    void testDateWorks_MatchingDay() {
        Availability availability = new Availability("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        LocalDate monday = LocalDate.of(2026, 1, 19); // Monday

        assertTrue(availability.dateWorks(monday));
    }

    @Test
    void testDateWorks_NonMatchingDay() {
        Availability availability = new Availability("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        LocalDate tuesday = LocalDate.of(2026, 1, 20); // Tuesday

        assertFalse(availability.dateWorks(tuesday));
    }

    @Test
    void testDateWorks_AllDaysOfWeek() {
        LocalDate startDate = LocalDate.of(2026, 1, 19); // Monday
        
        for (DayOfWeek day : DayOfWeek.values()) {
            Availability availability = new Availability(day.toString(), LocalTime.of(9, 0), LocalTime.of(17, 0));
            LocalDate dateForDay = startDate.with(day);
            assertTrue(availability.dateWorks(dateForDay), "Should match " + day);
        }
    }

    @Test
    void testTimeWorks_ExactStartTime() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(9, 0)));
    }

    @Test
    void testTimeWorks_ExactEndTime() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(17, 0)));
    }

    @Test
    void testTimeWorks_MiddleOfRange() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 30)));
    }

    @Test
    void testTimeWorks_BeforeStartTime() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(8, 59)));
    }

    @Test
    void testTimeWorks_AfterEndTime() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(17, 1)));
    }

    @Test
    void testTimeWorks_OneSecondBeforeStart() {
        LocalTime fromTime = LocalTime.of(9, 0, 0);
        LocalTime untilTime = LocalTime.of(17, 0, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(8, 59, 59)));
    }

    @Test
    void testTimeWorks_OneSecondAfterEnd() {
        LocalTime fromTime = LocalTime.of(9, 0, 0);
        LocalTime untilTime = LocalTime.of(17, 0, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(17, 0, 1)));
    }

    @Test
    void testTimeWorks_MidnightRange() {
        LocalTime fromTime = LocalTime.of(0, 0);
        LocalTime untilTime = LocalTime.of(23, 59, 59);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(0, 0)));
        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 0)));
        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(23, 59, 59)));
    }

    @Test
    void testTimeWorks_ShortRange() {
        LocalTime fromTime = LocalTime.of(12, 0);
        LocalTime untilTime = LocalTime.of(12, 1);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 0)));
        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 0, 30)));
        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 1)));
        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 1, 1)));
    }

    @Test
    void testTimeWorks_NoonRange() {
        LocalTime fromTime = LocalTime.of(11, 0);
        LocalTime untilTime = LocalTime.of(13, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.NOON));
    }

    @Test
    void testEquality_SameValues() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        
        Availability avail1 = new Availability("MONDAY", fromTime, untilTime);
        Availability avail2 = new Availability("MONDAY", fromTime, untilTime);

        assertEquals(avail1, avail2);
        assertEquals(avail1.hashCode(), avail2.hashCode());
    }

    @Test
    void testEquality_DifferentDays() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        
        Availability avail1 = new Availability("MONDAY", fromTime, untilTime);
        Availability avail2 = new Availability("TUESDAY", fromTime, untilTime);

        assertNotEquals(avail1, avail2);
    }

    @Test
    void testEquality_DifferentTimes() {
        Availability avail1 = new Availability("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        Availability avail2 = new Availability("MONDAY", LocalTime.of(10, 0), LocalTime.of(18, 0));

        assertNotEquals(avail1, avail2);
    }

    @Test
    void testToString() {
        LocalTime fromTime = LocalTime.of(9, 0);
        LocalTime untilTime = LocalTime.of(17, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        String toString = availability.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("MONDAY"));
        assertTrue(toString.contains("09:00"));
        assertTrue(toString.contains("17:00"));
    }

    @Test
    void testDateWorks_WithNullDayOfWeek() {
        Availability availability = new Availability(null, LocalTime.of(9, 0), LocalTime.of(17, 0));
        LocalDate monday = LocalDate.of(2026, 1, 19);

        // When dayOfWeek is null, equals() returns false
        assertFalse(availability.dateWorks(monday));
    }

    @Test
    void testTimeWorks_WithNanoseconds() {
        LocalTime fromTime = LocalTime.of(9, 0, 0, 123456789);
        LocalTime untilTime = LocalTime.of(17, 0, 0, 987654321);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(9, 0, 0, 123456789)));
        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(12, 0)));
        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(17, 0, 0, 987654321)));
        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(17, 0, 0, 987654322)));
    }

    @Test
    void testDateWorks_LeapYear() {
        Availability availability = new Availability("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        LocalDate leapDayMonday = LocalDate.of(2024, 2, 26); // Monday in leap year

        assertTrue(availability.dateWorks(leapDayMonday));
    }

    @Test
    void testTimeWorks_EarlyMorning() {
        LocalTime fromTime = LocalTime.of(0, 0);
        LocalTime untilTime = LocalTime.of(6, 0);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(3, 0)));
        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.of(6, 1)));
    }

    @Test
    void testTimeWorks_LateNight() {
        LocalTime fromTime = LocalTime.of(20, 0);
        LocalTime untilTime = LocalTime.of(23, 59, 59);
        Availability availability = new Availability("MONDAY", fromTime, untilTime);

        assertTrue(availability.timeWorks(fromTime, untilTime, LocalTime.of(22, 0)));
        assertFalse(availability.timeWorks(fromTime, untilTime, LocalTime.MIDNIGHT));
    }

    @Test
    void testDateWorks_Weekend() {
        Availability saturdayAvail = new Availability("SATURDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        Availability sundayAvail = new Availability("SUNDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        
        LocalDate saturday = LocalDate.of(2026, 1, 24);
        LocalDate sunday = LocalDate.of(2026, 1, 25);

        assertTrue(saturdayAvail.dateWorks(saturday));
        assertTrue(sundayAvail.dateWorks(sunday));
        assertFalse(saturdayAvail.dateWorks(sunday));
        assertFalse(sundayAvail.dateWorks(saturday));
    }
}
