package io.github.sandydunlop.markista.spi;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;

public interface DocService {
    void run(Api api, Context context);
}
