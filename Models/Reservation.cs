namespace CoworkingApp.Models
{
    public class Reservation
    {
        public int Id { get; set; }

        public int UserId { get; set; }
        public User User { get; set; }

        public int CreneauId { get; set; }
        public Creneau Creneau { get; set; }

        public DateTime DateReservation { get; set; } = DateTime.UtcNow;
    }
}
