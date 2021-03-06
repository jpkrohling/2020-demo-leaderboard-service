---
-- #%L
-- Leaderboard Aggregator Test
-- %%
-- Copyright (C) 2020 Red Hat Inc.,
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---
DROP TABLE IF EXISTS "public"."games" CASCADE ;
CREATE TABLE "public"."games" (
    "id" SERIAL,
    "game_id" character varying(75) NOT NULL,
    "game_config" jsonb,
    "game_date" timestamptz NOT NULL default now(),
    "game_state" smallint NOT NULL
) WITH (oids = false);


DROP TABLE IF EXISTS "public"."players";
CREATE TABLE "public"."players" (
    "id" SERIAL,
    "player_id" character varying(75) NOT NULL,
    "player_name" character varying(75) NOT NULL,
    "guess_right" smallint NOT NULL,
    "guess_wrong" smallint NOT NULL,
    "guess_score" smallint NOT NULL,
    "creation_server" character varying(50) NOT NULL,
    "game_server" character varying(50) NOT NULL,
    "scoring_server" character varying(50) NOT NULL,
    "player_avatar" jsonb NOT NULL,
    "game_id" character varying(75) NOT NULL
) WITH (oids = false);

-- Create Constraints

ALTER TABLE "public"."games" ADD CONSTRAINT "games_pkey"  PRIMARY KEY (id);

ALTER TABLE "public"."games" ADD CONSTRAINT "game_id_unique" UNIQUE (game_id);

ALTER TABLE "public"."players" ADD CONSTRAINT "player_pkey"  PRIMARY KEY (id);

ALTER TABLE "public"."players" ADD CONSTRAINT "player_id_unqiue"  UNIQUE (player_id);

ALTER TABLE "public"."players" ADD CONSTRAINT "players_game_id_fkey" FOREIGN KEY (game_id) REFERENCES games(game_id) NOT DEFERRABLE;

-- Create Indexes

DROP INDEX IF EXISTS "idx_games";
CREATE INDEX "idx_games" ON "public"."games" (game_date DESC);

DROP INDEX IF EXISTS "idx_game_id";
CREATE INDEX "idx_game_id" ON "public"."players" USING HASH (game_id);

DROP INDEX IF EXISTS  "idx_players";
CREATE INDEX "idx_players" ON "public"."players" (guess_score DESC,guess_right DESC, guess_wrong ASC);

