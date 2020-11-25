INSERT INTO entities (typename) VALUES ( 'single' ),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('ten'),
                            ('rand'),
                            ('rand'),
                            ('rand');

INSERT INTO attributes (datatype, name ) VALUES ( 'String', 'singleAttribute' ),
                                                ( 'String', 'tenAttribute' );

INSERT INTO eav_values (ent_id, att_id, value) VALUES ( 1,1, 'test' ),
                                                      ( 2,2, 'firstHalf' ),
                                                      ( 3,2, 'firstHalf' ),
                                                      ( 4,2, 'firstHalf' ),
                                                      ( 5,2, 'firstHalf' ),
                                                      ( 6,2, 'firstHalf' ),
                                                      ( 7,2, 'secondHalf' ),
                                                      ( 8,2, 'secondHalf' ),
                                                      ( 9,2, 'secondHalf' ),
                                                      ( 10,2, 'secondHalf' ),
                                                      ( 11,2, 'secondHalf' );
