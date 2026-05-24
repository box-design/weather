import { authRouter } from "./auth-router";
import { weatherRouter } from "./weather-router";
import { cityRouter } from "./city-router";
import { createRouter, publicQuery } from "./middleware";

export const appRouter = createRouter({
  ping: publicQuery.query(() => ({ ok: true, ts: Date.now() })),
  auth: authRouter,
  weather: weatherRouter,
  city: cityRouter,
});

export type AppRouter = typeof appRouter;
