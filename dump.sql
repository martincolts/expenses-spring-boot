--
-- PostgreSQL database dump
--

\restrict I7H0t4Pi1gRZXhh42bD3iFO7HD2xYq7Cmxs4o62BjcZ2iYLvdGcZGEXVu01zbsi

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: expense_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.expense_status AS ENUM (
    'PAID',
    'PENDING'
);


ALTER TYPE public.expense_status OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: category; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.category (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    icon_id character varying(50),
    color_id character varying(30)
);


ALTER TABLE public.category OWNER TO postgres;

--
-- Name: category_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.category_id_seq OWNER TO postgres;

--
-- Name: category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.category_id_seq OWNED BY public.category.id;


--
-- Name: expense; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.expense (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    category_id bigint,
    description character varying(255) NOT NULL,
    amount numeric(12,2) NOT NULL,
    currency character(3) NOT NULL,
    status public.expense_status DEFAULT 'PENDING'::public.expense_status NOT NULL,
    incurred_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    paid_at timestamp with time zone
);


ALTER TABLE public.expense OWNER TO postgres;

--
-- Name: expense_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.expense_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.expense_id_seq OWNER TO postgres;

--
-- Name: expense_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.expense_id_seq OWNED BY public.expense.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: income; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.income (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    amount numeric(12,2) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.income OWNER TO postgres;

--
-- Name: income_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.income_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.income_id_seq OWNER TO postgres;

--
-- Name: income_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.income_id_seq OWNED BY public.income.id;


--
-- Name: tenant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tenant (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    role character varying(20) DEFAULT 'TENANT'::character varying NOT NULL
);


ALTER TABLE public.tenant OWNER TO postgres;

--
-- Name: tenant_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tenant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tenant_id_seq OWNER TO postgres;

--
-- Name: tenant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tenant_id_seq OWNED BY public.tenant.id;


--
-- Name: category id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category ALTER COLUMN id SET DEFAULT nextval('public.category_id_seq'::regclass);


--
-- Name: expense id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense ALTER COLUMN id SET DEFAULT nextval('public.expense_id_seq'::regclass);


--
-- Name: income id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.income ALTER COLUMN id SET DEFAULT nextval('public.income_id_seq'::regclass);


--
-- Name: tenant id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant ALTER COLUMN id SET DEFAULT nextval('public.tenant_id_seq'::regclass);


--
-- Data for Name: category; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.category (id, tenant_id, name, created_at, icon_id, color_id) FROM stdin;
1	2	Compras / Supermercado	2026-06-07 03:16:08.9673+00	groceries	orange
2	2	Comidas fuera / Delivery	2026-06-07 03:16:08.9673+00	food	red
3	2	Hogar	2026-06-07 03:16:08.9673+00	home	teal
4	2	Transporte	2026-06-07 03:16:08.9673+00	car	blue
5	2	Salud y Bienestar	2026-06-07 03:16:08.9673+00	health	green
6	2	Trabajo y Profesión	2026-06-07 03:16:08.9673+00	work	indigo
7	2	Mascotas	2026-06-07 03:16:08.9673+00	pets	amber
8	2	Entretenimiento y Vida Social	2026-06-07 03:16:08.9673+00	movies	purple
9	2	Regalos y Celebraciones	2026-06-07 03:16:08.9673+00	gifts	pink
10	2	Gastos Grandes / Excepcionales	2026-06-07 03:16:08.9673+00	repairs	orange
11	2	Fricción Financiera	2026-06-07 03:16:08.9673+00	bills	gray
12	2	Seguro Auto	2026-06-07 03:16:08.9673+00	car	teal
13	2	Servicios	2026-06-07 03:55:24.192007+00	repairs	teal
14	2	Expensas	2026-06-07 03:56:18.826533+00	home	indigo
15	2	Tarjetas	2026-06-07 04:02:04.578517+00	bills	pink
17	2	Monotributo	2026-06-12 13:01:34.839216+00	work	pink
\.


--
-- Data for Name: expense; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.expense (id, tenant_id, category_id, description, amount, currency, status, incurred_at, created_at, paid_at) FROM stdin;
2	2	13	Luz	104000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 03:55:48.813137+00	2026-06-07 03:57:22.624741+00
4	2	5	Osde	359000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 03:58:03.213511+00	2026-06-07 03:58:03.213511+00
5	2	2	Helado	15700.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 03:58:42.65236+00	2026-06-07 03:58:42.65236+00
6	2	2	Mostaza, hamburguesas	27300.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 03:59:31.328438+00	2026-06-07 03:59:31.328438+00
7	2	13	Gas	73000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 04:00:17.020412+00	2026-06-07 04:00:17.020412+00
8	2	1	Monarca pedido	93000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 04:00:47.456708+00	2026-06-07 04:00:47.456708+00
9	2	8	Talleres	5000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 04:01:16.036076+00	2026-06-07 04:01:16.036076+00
10	2	15	Pago Visa	140854.00	ARS	PAID	2026-06-02 00:00:00+00	2026-06-07 04:02:33.94237+00	2026-06-07 04:02:33.94237+00
11	2	13	Internet, telefono, cable	106000.00	ARS	PAID	2026-06-04 00:00:00+00	2026-06-07 04:03:11.381386+00	2026-06-07 04:03:11.381386+00
12	2	1	jamon muza y salsa (caraculico)	14500.00	ARS	PAID	2026-06-05 00:00:00+00	2026-06-07 04:03:51.748149+00	2026-06-07 04:03:51.748149+00
13	2	1	Verduleria	2150.00	ARS	PAID	2026-06-05 00:00:00+00	2026-06-07 04:04:32.793803+00	2026-06-07 04:04:32.793803+00
14	2	1	caraculico	28000.00	ARS	PAID	2026-06-05 00:00:00+00	2026-06-07 04:05:03.284825+00	2026-06-07 04:05:03.284825+00
15	2	2	Helado	15700.00	ARS	PAID	2026-06-05 00:00:00+00	2026-06-07 04:05:23.412104+00	2026-06-07 04:05:23.412104+00
16	2	2	tequechizz	27500.00	ARS	PAID	2026-06-06 00:00:00+00	2026-06-07 04:05:44.216442+00	2026-06-07 04:05:44.216442+00
17	2	1	lo q quedó el mes anterior	650.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 04:06:41.206336+00	2026-06-07 04:06:41.206336+00
18	2	2	Vianda	11000.00	ARS	PAID	2026-06-07 00:00:00+00	2026-06-07 15:46:40.649497+00	2026-06-07 15:46:40.649497+00
19	2	1	Chino, hamburguesas 	22060.00	ARS	PAID	2026-06-07 00:00:00+00	2026-06-07 22:25:29.789585+00	2026-06-07 22:25:29.789585+00
20	2	1	Caraculico	7000.00	ARS	PAID	2026-06-08 00:00:00+00	2026-06-08 17:26:09.592196+00	2026-06-08 17:26:09.592196+00
21	2	2	Pizza ola	30300.00	ARS	PAID	2026-06-09 00:00:00+00	2026-06-09 23:31:52.309481+00	2026-06-09 23:31:52.309481+00
3	2	14	Expensas primera junta	81000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 03:57:19.141022+00	2026-06-10 23:37:01.179992+00
1	2	4	Cochera vicky	20000.00	ARS	PAID	2026-06-01 00:00:00+00	2026-06-07 03:54:37.181969+00	2026-06-10 23:37:04.513866+00
22	2	2	Tequechizz	24100.00	ARS	PAID	2026-06-10 00:00:00+00	2026-06-10 23:37:35.219242+00	2026-06-10 23:37:35.219242+00
23	2	2	Mostaza hamburguesas	27200.00	ARS	PAID	2026-06-12 00:00:00+00	2026-06-12 00:21:46.574073+00	2026-06-12 00:21:46.574073+00
25	2	12	Seguro Auto	96095.28	ARS	PAID	2026-06-12 00:00:00+00	2026-06-12 12:59:32.886943+00	2026-06-12 12:59:32.886943+00
24	2	9	Mini compresor, regalo del dia del padre	78500.00	ARS	PAID	2026-06-12 00:00:00+00	2026-06-12 02:10:27.584811+00	2026-06-12 02:10:27.584811+00
26	2	17	Monotributo	107234.10	ARS	PENDING	2026-06-12 00:00:00+00	2026-06-12 13:01:56.350305+00	\N
27	2	1	Caraculico	23500.00	ARS	PAID	2026-06-12 00:00:00+00	2026-06-12 22:46:43.656267+00	2026-06-12 22:46:43.656267+00
28	2	1	Verdulería 	3300.00	ARS	PAID	2026-06-12 00:00:00+00	2026-06-12 22:48:59.681895+00	2026-06-12 22:48:59.681895+00
29	2	2	Helado	15700.00	ARS	PAID	2026-06-12 00:00:00+00	2026-06-12 23:41:15.413572+00	2026-06-12 23:41:15.413572+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create expense table	SQL	V1__create_expense_table.sql	-1489477761	postgres	2026-06-07 03:00:54.699629	85	t
2	2	add role to tenant	SQL	V2__add_role_to_tenant.sql	-1794518207	postgres	2026-06-07 03:00:54.832037	5	t
3	3	create income table	SQL	V3__create_income_table.sql	1705431686	postgres	2026-06-07 03:00:54.858715	10	t
4	4	add icon color to category	SQL	V4__add_icon_color_to_category.sql	-832629691	postgres	2026-06-07 03:00:54.878831	3	t
5	5	cascade delete tenant	SQL	V5__cascade_delete_tenant.sql	-2107925337	postgres	2026-06-07 03:00:54.893912	19	t
6	6	rename expense status not paud yet to pending	SQL	V6__rename_expense_status_not_paud_yet_to_pending.sql	1641665469	postgres	2026-06-07 03:00:54.933152	7	t
\.


--
-- Data for Name: income; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.income (id, tenant_id, amount, created_at) FROM stdin;
1	2	2145000.00	2026-06-07 03:16:49.582221+00
\.


--
-- Data for Name: tenant; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tenant (id, name, email, password_hash, created_at, role) FROM stdin;
1	Administrator	admin@example.com	$2a$10$S3Eb7kFVzq8EvcsPTCFGBeaU8lMqm6raDvj999jkG.YB6./EuwUOa	2026-06-07 03:00:57.107372+00	ADMIN
2	martin lopez	martinlopez89.quequen@gmail.com	$2a$10$fXu99SVxmFlZ09/MU191E.cTNCBsefrEeRqOyKe5vLEy4WVZVRyl2	2026-06-07 03:16:08.89899+00	TENANT
\.


--
-- Name: category_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.category_id_seq', 17, true);


--
-- Name: expense_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.expense_id_seq', 29, true);


--
-- Name: income_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.income_id_seq', 4, true);


--
-- Name: tenant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tenant_id_seq', 2, true);


--
-- Name: category category_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_pkey PRIMARY KEY (id);


--
-- Name: expense expense_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: income income_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.income
    ADD CONSTRAINT income_pkey PRIMARY KEY (id);


--
-- Name: tenant tenant_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant
    ADD CONSTRAINT tenant_pkey PRIMARY KEY (id);


--
-- Name: category uq_category_tenant_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category
    ADD CONSTRAINT uq_category_tenant_name UNIQUE (tenant_id, name);


--
-- Name: tenant uq_tenant_email; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant
    ADD CONSTRAINT uq_tenant_email UNIQUE (email);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_category_tenant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_category_tenant ON public.category USING btree (tenant_id);


--
-- Name: idx_expense_category_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_expense_category_id ON public.expense USING btree (category_id);


--
-- Name: idx_expense_incurred_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_expense_incurred_at ON public.expense USING btree (incurred_at);


--
-- Name: idx_expense_tenant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_expense_tenant ON public.expense USING btree (tenant_id);


--
-- Name: idx_income_tenant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_income_tenant ON public.income USING btree (tenant_id);


--
-- Name: category category_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenant(id) ON DELETE CASCADE;


--
-- Name: expense expense_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: expense expense_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense
    ADD CONSTRAINT expense_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenant(id) ON DELETE CASCADE;


--
-- Name: income income_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.income
    ADD CONSTRAINT income_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenant(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict I7H0t4Pi1gRZXhh42bD3iFO7HD2xYq7Cmxs4o62BjcZ2iYLvdGcZGEXVu01zbsi

