PGDMP                     
    y            smartdoor_user    13.4    13.4     �           0    0    ENCODING    ENCODING        SET client_encoding = 'UTF8';
                      false            �           0    0 
   STDSTRINGS 
   STDSTRINGS     (   SET standard_conforming_strings = 'on';
                      false            �           0    0 
   SEARCHPATH 
   SEARCHPATH     8   SELECT pg_catalog.set_config('search_path', '', false);
                      false            �           1262    16452    smartdoor_user    DATABASE     m   CREATE DATABASE smartdoor_user WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE = 'English_Malaysia.1252';
    DROP DATABASE smartdoor_user;
                postgres    false                        3079    16456    pgcrypto 	   EXTENSION     <   CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
    DROP EXTENSION pgcrypto;
                   false            �           0    0    EXTENSION pgcrypto    COMMENT     <   COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';
                        false    2            �            1259    16453    smartdoor_user    TABLE     �   CREATE TABLE public.smartdoor_user (
    username character varying(70) NOT NULL,
    password character varying(100) NOT NULL,
    door_passcode character varying(100),
    door_status timestamp without time zone
);
 "   DROP TABLE public.smartdoor_user;
       public         heap    postgres    false            �          0    16453    smartdoor_user 
   TABLE DATA           X   COPY public.smartdoor_user (username, password, door_passcode, door_status) FROM stdin;
    public          postgres    false    201   <       �   !  x�E�Kr�0  е�½c%�q�����&|�G� QO�v��W����"y�D���Ăi�If�g�$�����I\�I �.�:쑎+S��!R����"�R���F�P���L�q����u
�o1k�3mtS.�D��>�A'��4�*GZ��q�p��/�b��Nj���ŘL����`	���,+�@����{`o8S5�0��^�`5.�)~o���ː��w��U�m��������G�׾4�l�ms��D���@����9�5� �|�1���fk     