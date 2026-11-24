package com.swyp.invite.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SampleController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void ApiException은_에러_코드의_상태와_본문으로_바뀐다() throws Exception {
        mvc.perform(get("/sample/closed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RSVP_CLOSED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.RSVP_CLOSED.getMessage()))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void 입력_검사에_걸리면_400과_틀린_필드를_준다() throws Exception {
        mvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    void 본문을_읽을_수_없으면_400이다() throws Exception {
        mvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void 파라미터의_제약에_걸리면_400과_파라미터_이름을_준다() throws Exception {
        mvc.perform(get("/sample/list").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void 파라미터에_제약이_있는_메서드에서도_본문의_틀린_필드를_준다() throws Exception {
        mvc.perform(post("/sample/both")
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void 파라미터의_타입이_맞지_않으면_400과_파라미터_이름을_준다() throws Exception {
        mvc.perform(get("/sample/list").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void 필수_파라미터가_없으면_400과_파라미터_이름을_준다() throws Exception {
        mvc.perform(get("/sample/list"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample/closed")
        void closed() {
            throw new ApiException(ErrorCode.RSVP_CLOSED);
        }

        @PostMapping("/sample")
        void create(@Valid @RequestBody SampleRequest request) {}

        @GetMapping("/sample/list")
        void list(@RequestParam @Min(1) int page) {}

        @PostMapping("/sample/both")
        void both(@RequestParam @Min(1) int page, @Valid @RequestBody SampleRequest request) {}
    }

    record SampleRequest(@NotBlank String name) {}
}
