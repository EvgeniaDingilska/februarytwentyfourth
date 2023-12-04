make_frame()
{
    java MakeHeader frames/$1.jpg 822b4bfb-c7c6-4b24-ac3a-ff85a8f3100e
    cp 822b4bfb-c7c6-4b24-ac3a-ff85a8f3100e.bin ../__files
    # sleep 0.010
}

while true; do
    for FRAME in {1..16}
    do
        make_frame $FRAME
    done
done