package com.preen.backend.rooms;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;

class RoomControllerTests {
    private final RoomService service = mock(RoomService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RoomController(service)).build();
    @Test void rejectsMalformedAndOverflowIdsWithoutQueryingDatabase() throws Exception {
        for (String id : List.of("0", "-1", "abc", "9223372036854775808"))
            mvc.perform(get("/api/accommodations/"+id+"/rooms")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void missingStayReturns404() throws Exception {
        when(service.list(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/accommodations/9/rooms")).andExpect(status().isNotFound());
    }
    @Test void existingStayCanHaveAnEmptyList() throws Exception {
        when(service.list(7L)).thenReturn(new RoomService.RoomList("7", "테스트 숙소", "숙소 설명", "제주 주소", "15:00", "11:00", List.of()));
        mvc.perform(get("/api/accommodations/7/rooms")).andExpect(status().isOk())
            .andExpect(jsonPath("$.accommodationId").value("7")).andExpect(jsonPath("$.rooms").isEmpty())
            .andExpect(jsonPath("$.address").value("제주 주소"))
            .andExpect(jsonPath("$.checkInTime").value("15:00"))
            .andExpect(jsonPath("$.checkOutTime").value("11:00"));
    }
}
