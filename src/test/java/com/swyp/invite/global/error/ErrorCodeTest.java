package com.swyp.invite.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

	@Test
	void 코드와_HTTP_상태가_backend_md의_에러_코드_표와_같다() throws IOException {
		Map<String, Integer> documented = Files.readAllLines(Path.of("docs/backend.md")).stream()
				.map(line -> line.split("\\|"))
				.filter(cells -> cells.length > 2 && cells[1].strip().matches("[A-Z_]+")
						&& cells[2].strip().matches("\\d{3}"))
				.collect(Collectors.toMap(cells -> cells[1].strip(), cells -> Integer.valueOf(cells[2].strip())));

		Map<String, Integer> actual = Arrays.stream(ErrorCode.values())
				.collect(Collectors.toMap(Enum::name, ErrorCode::getStatus));

		assertThat(actual).isEqualTo(documented);
	}

}
