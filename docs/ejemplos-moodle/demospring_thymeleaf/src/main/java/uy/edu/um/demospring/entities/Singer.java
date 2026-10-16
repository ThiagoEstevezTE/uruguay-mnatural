package uy.edu.um.demospring.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

import static jakarta.persistence.GenerationType.IDENTITY;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "SINGER")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Singer implements Serializable
{

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Temporal(TemporalType.DATE)
    @Column(name = "BIRTH_DATE")
    private Date birthDate;


    @OneToMany(mappedBy = "singer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Album> albums = new LinkedList<Album>();



    public boolean addAlbum(Album album)
    {
        album.setSinger(this);
        return getAlbums().add(album);
    }

    public void removeAlbum(Album album)
    {
        getAlbums().remove(album);
    }

    @Override
    public String toString()
    {
        return (new StringBuilder())
                .append("\tSinger{Id: ")
                .append(id)
                .append("\t, First name: ")
                .append(firstName)
                .append("\t, Last name: ")
                .append(lastName)
                .append("\t, Birthday: ")
                .append( birthDate )
                .append("}")
                .toString();
    }
}