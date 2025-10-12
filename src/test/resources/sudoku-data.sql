
COPY public.grid (grid_id) FROM stdin;
1
2
\.

SELECT pg_catalog.setval('public.grid_id_seq', 2, true);

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

COPY public.cell (cell_id, grid_id, row_number, column_number, cell_value) FROM stdin;
27	2	0	1	2
28	2	0	4	8
29	2	0	7	1
30	2	1	3	4
31	2	1	4	5
32	2	1	5	9
33	2	1	8	2
34	2	2	0	6
35	2	2	3	1
36	2	2	7	3
37	2	3	0	1
38	2	3	4	9
39	2	3	6	7
40	2	4	1	5
41	2	4	2	3
42	2	4	6	6
43	2	4	7	2
44	2	5	2	7
45	2	5	4	6
46	2	5	8	8
47	2	6	1	1
48	2	6	5	5
49	2	6	8	4
50	2	7	0	2
51	2	7	3	8
52	2	7	4	4
53	2	7	5	7
54	2	8	1	8
55	2	8	4	2
56	2	8	7	7
\.

SELECT pg_catalog.setval('public.cell_id_seq', 56, true);

COPY public.sudoku (sudoku_id, start_grid_id) FROM stdin;
1	1
2	2
\.

SELECT pg_catalog.setval('public.sudoku_id_seq', 2, true);
