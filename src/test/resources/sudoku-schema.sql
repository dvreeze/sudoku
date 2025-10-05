
ALTER SCHEMA public OWNER TO postgres;

CREATE DOMAIN public."bıgınt" AS bigint;

ALTER DOMAIN public."bıgınt" OWNER TO postgres;

CREATE DOMAIN public.digit AS integer
	CONSTRAINT digit_check CHECK (((VALUE >= 1) AND (VALUE <= 9)));

ALTER DOMAIN public.digit OWNER TO postgres;

CREATE DOMAIN public.row_idx AS integer
	CONSTRAINT row_check CHECK (((VALUE >= 0) AND (VALUE <= 8)));

ALTER DOMAIN public.row_idx OWNER TO postgres;

CREATE DOMAIN public.col_idx AS integer
	CONSTRAINT col_check CHECK (((VALUE >= 0) AND (VALUE <= 8)));

ALTER DOMAIN public.col_idx OWNER TO postgres;

--
-- Table grid
--

CREATE SEQUENCE public.grid_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.grid_id_seq OWNER TO postgres;

CREATE TABLE public.grid (
    grid_id bigint DEFAULT nextval('public.grid_id_seq'::regclass) NOT NULL
);

ALTER TABLE public.grid OWNER TO postgres;

ALTER TABLE ONLY public.grid
    ADD CONSTRAINT grid_pkey PRIMARY KEY (grid_id);

--
-- Table cell
--

CREATE SEQUENCE public.cell_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.cell_id_seq OWNER TO postgres;

CREATE TABLE public.cell (
    cell_id bigint DEFAULT nextval('public.cell_id_seq'::regclass) NOT NULL,
    grid_id bigint NOT NULL,
    row_number row_idx NOT NULL,
    column_number col_idx NOT NULL,
    cell_value digit NULL
);

ALTER TABLE public.cell OWNER TO postgres;

ALTER TABLE ONLY public.cell
    ADD CONSTRAINT cell_pkey PRIMARY KEY (cell_id);

ALTER TABLE ONLY public.cell
    ADD CONSTRAINT cell_grid_id_fkey FOREIGN KEY (grid_id) REFERENCES public.grid(grid_id);

CREATE INDEX cell_grid_idx ON public.cell USING btree (grid_id);

--
-- Table sudoku
--

CREATE SEQUENCE public.sudoku_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.sudoku_id_seq OWNER TO postgres;

CREATE TABLE public.sudoku (
    sudoku_id bigint DEFAULT nextval('public.sudoku_id_seq'::regclass) NOT NULL,
    start_grid_id bigint NOT NULL
);

ALTER TABLE public.sudoku OWNER TO postgres;

ALTER TABLE ONLY public.sudoku
    ADD CONSTRAINT sudoku_pkey PRIMARY KEY (sudoku_id);

ALTER TABLE ONLY public.sudoku
    ADD CONSTRAINT sudoku_start_grid_id_fkey FOREIGN KEY (start_grid_id) REFERENCES public.grid(grid_id);

--
-- Table game_history
--

CREATE SEQUENCE public.game_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.game_history_id_seq OWNER TO postgres;

CREATE TABLE public.game_history (
    game_history_id bigint DEFAULT nextval('public.game_history_id_seq'::regclass) NOT NULL,
    sudoku_id bigint NOT NULL,
    player varchar(255) NOT NULL,
    start_time timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.game_history OWNER TO postgres;

ALTER TABLE ONLY public.game_history
    ADD CONSTRAINT game_history_pkey PRIMARY KEY (game_history_id);

ALTER TABLE ONLY public.game_history
    ADD CONSTRAINT game_history_sudoku_id_fkey FOREIGN KEY (sudoku_id) REFERENCES public.sudoku(sudoku_id);

--
-- Table step
--

CREATE TABLE public.step (
    game_history_id bigint NOT NULL,
    step_seq_number integer NOT NULL,
    row_number row_idx NOT NULL,
    column_number col_idx NOT NULL,
    step_value digit NOT NULL
);

ALTER TABLE public.step OWNER TO postgres;

ALTER TABLE ONLY public.step
    ADD CONSTRAINT step_pkey PRIMARY KEY (game_history_id, step_seq_number);

ALTER TABLE ONLY public.step
    ADD CONSTRAINT step_game_history_id_fkey FOREIGN KEY (game_history_id) REFERENCES public.game_history(game_history_id);
