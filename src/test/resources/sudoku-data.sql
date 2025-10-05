
COPY public.grid (grid_id) FROM stdin;
1
\.

SELECT pg_catalog.setval('public.grid_id_seq', 1, true);

COPY public.cell (cell_id, grid_id, row_number, column_number, cell_value) FROM stdin;
1	1	0	1	1
2	1	0	3	5
3	1	0	4	6
4	1	0	6	9
5	1	1	4	2
6	1	1	6	8
7	1	1	8	6
8	1	2	1	5
9	1	2	5	7
10	1	2	7	3
11	1	3	1	4
12	1	4	3	1
13	1	4	5	9
14	1	5	0	9
15	1	5	2	8
16	1	5	6	6
17	1	6	2	9
18	1	6	6	5
19	1	6	7	8
20	1	6	8	4
21	1	7	1	3
22	1	7	2	4
23	1	7	3	8
24	1	8	4	1
25	1	8	5	5
26	1	8	7	9
\.

SELECT pg_catalog.setval('public.cell_id_seq', 26, true);

COPY public.sudoku (sudoku_id, start_grid_id) FROM stdin;
1	1
\.

SELECT pg_catalog.setval('public.sudoku_id_seq', 1, true);
