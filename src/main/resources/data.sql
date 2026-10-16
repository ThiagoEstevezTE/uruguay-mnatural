-- Corre en cada arranque. ON CONFLICT DO NOTHING: si ya estan cargados (base que no se borra, como Supabase), no se duplican.
INSERT INTO departments (name) VALUES
                                   ('Artigas'), ('Canelones'), ('Cerro Largo'), ('Colonia'), ('Durazno'),
                                   ('Flores'), ('Florida'), ('Lavalleja'), ('Maldonado'), ('Montevideo'),
                                   ('Paysandú'), ('Río Negro'), ('Rivera'), ('Rocha'), ('Salto'),
                                   ('San José'), ('Soriano'), ('Tacuarembó'), ('Treinta y Tres')
ON CONFLICT DO NOTHING;

-- Coordenadas (latitud, longitud) para consultar el pronostico del clima
INSERT INTO localities (name, department_id, latitude, longitude) VALUES
    ('Artigas', 1, -30.4000, -56.4667),
    ('Bella Unión', 1, -30.2597, -57.5992),
    ('Canelones', 2, -34.5228, -56.2778),
    ('Atlántida', 2, -34.7719, -55.7586),
    ('Melo', 3, -32.3667, -54.1667),
    ('Colonia del Sacramento', 4, -34.4711, -57.8442),
    ('Carmelo', 4, -34.0000, -58.2833),
    ('Durazno', 5, -33.3806, -56.5236),
    ('Trinidad', 6, -33.5167, -56.9000),
    ('Florida', 7, -34.0956, -56.2142),
    ('Minas', 8, -34.3700, -55.2372),
    ('Villa Serrana', 8, -34.3167, -54.9833),
    ('Maldonado', 9, -34.9000, -54.9500),
    ('Punta del Este', 9, -34.9600, -54.9400),
    ('Piriápolis', 9, -34.8650, -55.2750),
    ('Montevideo', 10, -34.9011, -56.1645),
    ('Paysandú', 11, -32.3214, -58.0756),
    ('Fray Bentos', 12, -33.1325, -58.2956),
    ('Rivera', 13, -30.9053, -55.5508),
    ('Rocha', 14, -34.4833, -54.3333),
    ('La Paloma', 14, -34.6625, -54.1639),
    ('Cabo Polonio', 14, -34.4000, -53.7833),
    ('Punta del Diablo', 14, -34.0417, -53.5444),
    ('Salto', 15, -31.3833, -57.9667),
    ('Termas del Daymán', 15, -31.4500, -57.9167),
    ('San José de Mayo', 16, -34.3375, -56.7136),
    ('Mercedes', 17, -33.2524, -58.0305),
    ('Tacuarembó', 18, -31.7333, -55.9833),
    ('Treinta y Tres', 19, -33.2333, -54.3833)
ON CONFLICT DO NOTHING;
