import { z } from "zod";
import { eq, and } from "drizzle-orm";
import { createRouter, publicQuery, authedQuery } from "./middleware";
import { getDb } from "./queries/connection";
import { savedCities } from "@db/schema";

export const cityRouter = createRouter({
  list: publicQuery.query(async ({ ctx }) => {
    if (!ctx.user) return [];
    const db = getDb();
    return db
      .select()
      .from(savedCities)
      .where(eq(savedCities.userId, ctx.user.id))
      .orderBy(savedCities.createdAt);
  }),

  add: authedQuery
    .input(
      z.object({
        name: z.string().min(1),
        latitude: z.number(),
        longitude: z.number(),
        country: z.string().optional(),
        timezone: z.string().optional(),
      })
    )
    .mutation(async ({ ctx, input }) => {
      const db = getDb();
      const [result] = await db.insert(savedCities).values({
        name: input.name,
        latitude: input.latitude.toFixed(7),
        longitude: input.longitude.toFixed(7),
        country: input.country ?? null,
        timezone: input.timezone ?? null,
        userId: ctx.user.id,
        isDefault: "0",
      });
      return { id: Number(result.insertId), ...input };
    }),

  remove: authedQuery
    .input(z.object({ id: z.number() }))
    .mutation(async ({ ctx, input }) => {
      const db = getDb();
      await db
        .delete(savedCities)
        .where(
          and(
            eq(savedCities.id, input.id),
            eq(savedCities.userId, ctx.user.id)
          )
        );
      return { success: true };
    }),

  setDefault: authedQuery
    .input(z.object({ id: z.number() }))
    .mutation(async ({ ctx, input }) => {
      const db = getDb();
      await db
        .update(savedCities)
        .set({ isDefault: "0" })
        .where(eq(savedCities.userId, ctx.user.id));
      await db
        .update(savedCities)
        .set({ isDefault: "1" })
        .where(
          and(
            eq(savedCities.id, input.id),
            eq(savedCities.userId, ctx.user.id)
          )
        );
      return { success: true };
    }),
});
