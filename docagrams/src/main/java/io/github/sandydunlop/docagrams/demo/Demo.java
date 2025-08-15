package io.github.sandydunlop.docagrams.demo;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.spi.DocService;

public class Demo implements DocService {
    public Demo() {
        // Nothing to see here
    }

    @Override
    public void run(Api api, Context context) {
        System.out.println("run");
    }
}
